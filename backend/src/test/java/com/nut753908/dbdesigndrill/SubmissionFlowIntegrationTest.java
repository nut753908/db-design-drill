package com.nut753908.dbdesigndrill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nut753908.dbdesigndrill.dto.GenerateProblemRequest;
import com.nut753908.dbdesigndrill.dto.GenerateProblemResponse;
import com.nut753908.dbdesigndrill.dto.ReviewDesignRequest;
import com.nut753908.dbdesigndrill.dto.ReviewDesignResponse;
import com.nut753908.dbdesigndrill.dto.ReviewImplementationRequest;
import com.nut753908.dbdesigndrill.dto.ReviewImplementationResponse;
import com.nut753908.dbdesigndrill.repository.DesignSubmissionRepository;
import com.nut753908.dbdesigndrill.repository.ImplementationSubmissionRepository;
import com.nut753908.dbdesigndrill.service.LambdaInvoker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DDL提出→設計レビュー→実装提出→実装レビューという一連のフローを、実際のHTTPリクエスト経由・実際のPostgreSQL
 * に対して検証する。
 *
 * <p>Testcontainersはこの開発環境のDocker Engine(API 1.40以上のみサポート)と、testcontainers/docker-java
 * が内部の疎通確認で固定使用するAPIバージョン(1.32)が非互換のため使用できなかった。代わりに
 * docker-compose(db-design-drill-db-1)側にあらかじめ作成した専用のテストDB(dbdesigndrill_test、
 * src/test/resources/application-test.yml)に接続する。実行前に `docker compose up -d db` が必要。
 *
 * <p>あえて@Transactionalを付けていない。テストを1トランザクションで包むと各リクエスト間でHibernateセッションが
 * 共有され続けてしまい、本番同様に「リクエストごとにセッションが閉じる」状況(open-in-view: false)を再現できず、
 * 過去に実際に発生したLazyInitializationExceptionを検知できなくなる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DesignSubmissionRepository designSubmissionRepository;

    @Autowired
    private ImplementationSubmissionRepository implementationSubmissionRepository;

    @MockBean
    private LambdaInvoker lambdaInvoker;

    @Test
    void submitDesignAndImplementation_roundTripsLongTextWithoutError() throws Exception {
        // 生成AIのレスポンスを模したそこそこ長いテキスト。@Lobをoid型にマッピングしてしまう不具合は
        // 短い文字列でも顕在化するが、TEXT型への変換漏れなど切り詰め系の不具合にも気付けるよう長めにしておく。
        String requirementText = "要件文の本文です。".repeat(500);
        String ddlText = "CREATE TABLE members (id BIGSERIAL PRIMARY KEY);\n".repeat(200);
        String designReviewComment = "設計レビューコメントです。".repeat(200);
        String modelAnswer = "模範解答のDDLです。".repeat(200);
        String codeText = "@Entity public class Member { }\n".repeat(200);
        String implementationReviewComment = "実装レビューコメントです。".repeat(200);

        when(lambdaInvoker.invoke(any(GenerateProblemRequest.class), eq(GenerateProblemResponse.class)))
                .thenReturn(new GenerateProblemResponse(requirementText));
        when(lambdaInvoker.invoke(any(ReviewDesignRequest.class), eq(ReviewDesignResponse.class)))
                .thenReturn(new ReviewDesignResponse(designReviewComment, modelAnswer));
        when(lambdaInvoker.invoke(any(ReviewImplementationRequest.class), eq(ReviewImplementationResponse.class)))
                .thenReturn(new ReviewImplementationResponse(implementationReviewComment));

        String problemLocation = mockMvc.perform(post("/problems")
                        .param("genre", "EC")
                        .param("difficulty", "BEGINNER"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        Long problemId = extractId(problemLocation);

        String designSubmissionLocation = mockMvc.perform(post("/problems/{problemId}/design-submissions", problemId)
                        .param("ddlText", ddlText))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        Long designSubmissionId = extractId(designSubmissionLocation);

        // ここが今回追加した回帰テストの核心: 修正前はDesignSubmission.problemの遅延ロードが
        // トランザクション外で初期化されずLazyInitializationExceptionが発生し、500(エラーページ)になっていた。
        String implementationSubmissionLocation = mockMvc.perform(post(
                                "/design-submissions/{designSubmissionId}/implementation-submissions",
                                designSubmissionId)
                        .param("codeText", codeText))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        Long implementationSubmissionId = extractId(implementationSubmissionLocation);

        mockMvc.perform(get("/implementation-submissions/{id}", implementationSubmissionId))
                .andExpect(status().isOk());

        // お題一覧の取得(ProblemController.show -> findByProblemIdOrderByCreatedAtDesc)は、
        // @LobがPostgreSQL上でoid型にマッピングされていた際に「Large Objects may not be used in
        // auto-commit mode」が発生していた箇所そのもの。
        mockMvc.perform(get("/problems/{id}", problemId)).andExpect(status().isOk());

        // TEXT型への変更(旧: @Lob → PostgreSQL上でoid型)により、長文が欠損・切り詰めなく往復することを確認する。
        var designSubmission = designSubmissionRepository.findById(designSubmissionId).orElseThrow();
        assertThat(designSubmission.getDdlText()).isEqualTo(ddlText);
        assertThat(designSubmission.getReviewComment()).isEqualTo(designReviewComment);
        assertThat(designSubmission.getModelAnswer()).isEqualTo(modelAnswer);

        var implementationSubmission =
                implementationSubmissionRepository.findById(implementationSubmissionId).orElseThrow();
        assertThat(implementationSubmission.getCodeText()).isEqualTo(codeText);
        assertThat(implementationSubmission.getReviewComment()).isEqualTo(implementationReviewComment);
    }

    private static Long extractId(String redirectedUrl) {
        assertThat(redirectedUrl).isNotNull();
        String[] segments = redirectedUrl.split("/");
        return Long.valueOf(segments[segments.length - 1]);
    }
}
