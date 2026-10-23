---
name: grilling
description: Use when the user asks to be grilled, requests "Grill Me", or wants to stress-test a plan, decision, or idea. Requests to install or edit this skill do not start an interview.
metadata:
  opencode/slash: "true"
---

# Grill Me

Interview the user until you reach a shared understanding. Map the topic as a decision tree: each decision branches into decisions that depend on it. Challenge assumptions respectfully and investigate facts yourself.

## One question at a time

1. Choose the highest-value unsettled decision whose prerequisites are resolved. Ask exactly one question about it, even when several independent decisions are ready.
2. Offer 2-3 concrete, mutually exclusive choices when useful. Put the recommended choice first, mark it Recommended, and briefly explain each tradeoff. Allow a free-text answer; do not force a preference into unsuitable choices.
3. Use the agent's available question interface:
   - In actual Plan Mode, use `request_user_input` if available, with exactly one question.
   - Otherwise, prefer `request_user_input_async` if available, with exactly one question.
   - Other agents may expose a native question tool under another name. Use its documented schema with one question.
   - If no question tool is available or a dialog cannot be used, ask the single question in chat, include the choices, invite a free-text answer, and end the turn.
   - If the user explicitly requires dialogs only, explain any missing dialog capability and pause instead of falling back to chat.
4. Wait for the user's submitted answer. An asynchronous tool returning, elapsed time, an empty response, or a preselected option is not an answer. Independent factual research may continue while waiting.
5. After an answer, briefly acknowledge the decision, update the decision tree, and select the next question. Keep additional decisions out of the question, options, and acknowledgment.

Keep only one unanswered interview question pending. Never batch questions or embed a follow-up inside an option.

Respect user-specified question limits and depth. Count each interview question, including a final confirmation, toward the limit. At the limit, summarize settled decisions and unresolved assumptions; wait for explicit direction instead of silently filling gaps.

## Investigate facts and resolve decisions

Discoverable facts are the agent's job. Use available read and search tools. Delegate independent research when subagents are available and useful; otherwise investigate directly. Tool names and delegation support differ between agents and models.

Treat an ongoing investigation as an unsettled prerequisite. Another ready decision may supply the next question. Recompute the decision tree after each answer and ask dependent questions only when their prerequisites are settled.

Finish when the decisions are resolved or the question limit is reached. Summarize the shared understanding and open decisions. If the user wants implementation and the limit permits, obtain confirmation with one question using the available interface. If no question remains, summarize and wait. Grilling alone does not authorize implementation.
