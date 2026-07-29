// db-design-drill をヘッドレスChromiumで実際に操作するドライバ。
// お題作成 → DDL提出(設計レビュー) → JPA実装提出(実装レビュー) → 履歴反映
// という一連のフローを最後まで叩き、各画面のスクリーンショットを残す。
import { chromium } from "playwright";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const BASE_URL = process.env.BASE_URL ?? "http://localhost:8080";
const SHOT_DIR = process.env.SHOT_DIR ?? path.join(__dirname, "shots");

const DDL = `CREATE TABLE customers (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE products (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  price INT NOT NULL,
  stock INT NOT NULL
);

CREATE TABLE orders (
  id BIGINT PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES customers(id),
  ordered_at TIMESTAMP NOT NULL,
  total_price INT NOT NULL
);

CREATE TABLE order_items (
  id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL REFERENCES orders(id),
  product_id BIGINT NOT NULL REFERENCES products(id),
  quantity INT NOT NULL,
  unit_price INT NOT NULL
);`;

const JPA_CODE = `@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private Long id;
    private String name;
    private String email;
}`;

// Lambda(Gemini API)呼び出しを伴う送信は、Geminiの一時的な過負荷で
// たまに502(LambdaInvocationException)が返ることがある。502のときは
// 同じフォームのページからfillFn→クリックをやり直せば大抵成功するため、
// フォーム送信全体をリトライするヘルパー。
async function fillAndSubmitWithRetry(page, formUrl, fillFn, buttonText, urlPattern, attempts = 3) {
  for (let i = 1; i <= attempts; i++) {
    await page.goto(formUrl);
    await fillFn(page);
    const [response] = await Promise.all([
      page.waitForResponse((res) => res.request().method() === "POST"),
      page.click(`button:has-text("${buttonText}")`),
    ]);
    // 正常系はPOST→302リダイレクト→GETなので2xxではなく3xxで返る。
    // 502(LambdaInvocationException)などの5xxのみリトライ対象とする。
    if (response.status() < 500) {
      await page.waitForURL(urlPattern, { timeout: 30000 });
      return;
    }
    console.log(`submit "${buttonText}" attempt ${i} failed with HTTP ${response.status()}, retrying...`);
    await page.waitForTimeout(2000);
  }
  throw new Error(`submit "${buttonText}" failed after ${attempts} attempts`);
}

async function main() {
  const browser = await chromium.launch({ args: ["--no-sandbox"] });
  const page = await browser.newPage({ viewport: { width: 1000, height: 900 } });
  // 502(Gemini過負荷によるLambdaInvocationException)はfillAndSubmitWithRetryが
  // リトライして吸収するので、ここでは参考ログとして出すだけで失敗扱いにはしない。
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      console.log("CONSOLE ERROR (informational):", msg.text());
    }
  });

  // Step0: 履歴一覧(開始前)
  await page.goto(`${BASE_URL}/history`);
  await page.screenshot({ path: `${SHOT_DIR}/0_history_before.png`, fullPage: true });

  // Step1: お題作成
  await page.goto(`${BASE_URL}/problems/new`);
  await page.screenshot({ path: `${SHOT_DIR}/1_new.png`, fullPage: true });
  await page.selectOption('select[name="genre"]', { index: 0 });
  await page.selectOption('select[name="difficulty"]', { index: 0 });
  await page.click('button:has-text("お題を生成する")');
  await page.waitForURL(/\/problems\/\d+/, { timeout: 30000 });
  await page.waitForSelector("text=要件");
  await page.screenshot({ path: `${SHOT_DIR}/2_problem_show.png`, fullPage: true });
  const problemUrl = page.url();
  console.log("PROBLEM_URL:", problemUrl);

  // Step2: DDL提出 → 設計レビュー(Gemini呼び出し。502なら再送信)
  await fillAndSubmitWithRetry(
    page,
    problemUrl,
    (p) => p.fill('textarea[name="ddlText"]', DDL),
    "設計をレビューする",
    /\/design-submissions\/\d+/
  );
  await page.waitForSelector("text=AIレビュー");
  await page.screenshot({ path: `${SHOT_DIR}/3_design_review.png`, fullPage: true });
  const designUrl = page.url();
  console.log("DESIGN_URL:", designUrl);

  // Step3: JPA実装提出 → 実装レビュー(Gemini呼び出し。502なら再送信)
  await fillAndSubmitWithRetry(
    page,
    designUrl,
    (p) => p.fill('textarea[name="codeText"]', JPA_CODE),
    "実装をレビューする",
    /\/implementation-submissions\/\d+/
  );
  await page.waitForSelector("text=AIレビュー");
  await page.screenshot({ path: `${SHOT_DIR}/4_implementation_review.png`, fullPage: true });
  console.log("IMPL_URL:", page.url());

  // Step4: 履歴一覧(完了後)
  await page.goto(`${BASE_URL}/history`);
  await page.screenshot({ path: `${SHOT_DIR}/5_history_after.png`, fullPage: true });

  await browser.close();
  console.log("DONE");
}

main().catch((e) => {
  console.error("FAILED:", e);
  process.exit(1);
});
