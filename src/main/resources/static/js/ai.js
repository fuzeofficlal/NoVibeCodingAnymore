import { api } from "./api.js";
import { marked } from "https://cdn.jsdelivr.net/npm/marked/lib/marked.esm.js";
import {
  bindModal,
  getAdvisorKey,
  getPortfolioContext,
  saveAdvisorKey,
  setText,
  showModal,
  toast
} from "./ui.js";

async function runInsight() {
  const context = getPortfolioContext();
  const apiKey = document.getElementById("advisorApiKey").value.trim();
  if (apiKey) saveAdvisorKey(apiKey);
  saveAdvisorKey(apiKey);
  try {
    const result = await api.getInsight(context.id, apiKey);
    const resultText = typeof result === "string" ? result : JSON.stringify(result, null, 2);
    document.getElementById("advisorInsight").innerHTML = marked.parse(resultText);
    toast("AI risk insight loaded.");
  } catch (error) {
    showModal("Insight failed", error.message, "error");
  }
}

async function runChat(event) {
  event.preventDefault();
  const context = getPortfolioContext();
  const apiKey = document.getElementById("advisorApiKey").value.trim();
  const query = document.getElementById("advisorPrompt").value.trim();
  if (!query) {
    showModal("Missing input", "Enter a request for the advisor.", "error");
    return;
  }
  if (apiKey) saveAdvisorKey(apiKey);
  try {
    const result = await api.chatWithAdvisor(context.id, apiKey, query);
    const resultText = typeof result === "string" ? result : JSON.stringify(result, null, 2);
    document.getElementById("advisorChatResult").innerHTML = marked.parse(resultText);
    toast("Advisor response received.");
  } catch (error) {
    showModal("Chat failed", error.message, "error");
  }
}

function init() {
  bindModal();
  const context = getPortfolioContext();
  setText("aiPortfolioId", context.id || "--");
  if (!context.id) {
    document.getElementById("advisorInsight").textContent = "AI page is open. Create or resume a portfolio to run portfolio-specific insight and chat actions.";
    document.getElementById("advisorChatResult").textContent = "Portfolio context is required before backend advisor calls can run.";
  }
  document.getElementById("advisorApiKey").value = getAdvisorKey();
  document.getElementById("loadInsight")?.addEventListener("click", runInsight);
  document.getElementById("advisorChatForm")?.addEventListener("submit", runChat);
}

init();
