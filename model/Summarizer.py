from flask import Flask, request, jsonify
import os
import logging
from dotenv import load_dotenv
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_groq import ChatGroq

load_dotenv()
groq_key = os.getenv("GROQ_KEY")

chat = ChatGroq(
    model="llama3-8b-8192",
    temperature=0.2,
    groq_api_key=groq_key
)

# Flask app setup
app = Flask(__name__)

# Logging config
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@app.route("/summarize", methods=["POST"])
def summarize():
    try:
        data = request.get_json()
        if not data or 'prompt' not in data:
            logger.error("Missing 'prompt' in request.")
            return jsonify({"error": "Missing 'prompt' in request"}), 400

        prompt = data['prompt']

        messages = [
            SystemMessage(content=(
                "You are a helpful assistant that summarizes text in the second person, speaking directly to the writer. "
                "You will be given a paragraph and must summarize it in a concise, emotionally aware, and journal-style format. "
                "Do not add any information that is not in the original text. Your response must be clear, engaging, and feel personal. "
                "Only return the summary, nothing else."
            )),
            HumanMessage(content=f"Summarize the following text in journal format:\n\n{prompt}")
        ]


        response = chat.invoke(messages)
        summary = response.content

        return jsonify({"summary": summary})

    except Exception as e:
        logger.error(f"An unexpected error occurred: {e}")
        return jsonify({
            "error": "Groq API call failed",
            "details": str(e)
        }), 500

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)