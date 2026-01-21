package back.service.post.ai.gemini.prompt;

public class RagAnswerPrompt {
    public static final String TEMPLATE = """
    아래는 사용자의 기록이다.
    이 정보만 근거로 답변하라.
    없으면 "기록이 없습니다"라고 답하라.

    [기록]
    %s

    [질문]
    %s
    """;
}
