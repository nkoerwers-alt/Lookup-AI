
export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("Fenn AI backend is running.");
    }

    try {
      const body = await request.json();

      if (!body.question) {
        return Response.json(
          { error: "Missing question." },
          { status: 400 }
        );
      }

      const response = await fetch(
        "https://api.groq.com/openai/v1/chat/completions",
        {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${env.GROQ_API_KEY}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            model: "openai/gpt-oss-120b",
            messages: [
              {
                role: "user",
                content: body.question
              }
            ]
          })
        }
      );

      const data = await response.json();

      if (!response.ok) {
        return Response.json(
          { error: data.error?.message || "AI request failed." },
          { status: response.status }
        );
      }

      return Response.json({
        answer: data.choices[0].message.content
      });

    } catch (error) {
      return Response.json(
        { error: "Backend error." },
        { status: 500 }
      );
    }
  }
};
