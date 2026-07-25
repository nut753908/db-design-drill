// AIレビュー等、Lambda呼び出しを伴うフォーム送信時に、応答待ちであることを示すローディング表示を出す。
// サーバー呼び出し自体は同期のままのため見た目だけの改善だが、フォームの二重送信も防げる。
document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll("form.js-loading-form").forEach(function (form) {
    form.addEventListener("submit", function () {
      var button = form.querySelector('button[type="submit"]');
      if (!button || button.disabled) {
        return;
      }
      button.disabled = true;
      button.textContent = "処理中...";
      button.classList.add("is-loading");
    });
  });
});
