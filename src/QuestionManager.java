import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class QuestionManager {

    private static final String BASE_PATH = "questions/"; // Directory where JSON files are stored

    /**
     * Retrieves a random question from the JSON file corresponding to the given
     * category and difficulty.
     * 
     * @param category   The category of the question.
     * @param difficulty The difficulty level.
     * @return A map containing the question, answers, and correct answer, or null
     *         if no questions found.
     */
    public static Map<String, Object> getRandomQuestion(String category, String difficulty) {
        String filePath = BASE_PATH + "questions_" + category + "_" + difficulty + ".json"; // Builds the correct file
                                                                                            // path

        List<Map<String, Object>> questionsList = readQuestions(filePath); // Reads all questions from the file

        if (questionsList == null || questionsList.isEmpty()) { // If there are no questions
            System.out.println("No questions found in file: " + filePath);
            return null;
        }

        // Selects a random question from the list
        Random random = new Random();
        return questionsList.get(random.nextInt(questionsList.size()));
    }

    /**
     * Reads questions from a JSON file and returns a list of question data.
     * 
     * @param filePath The path to the JSON file.
     * @return A list of question maps containing the question, answers, and correct
     *         answer.
     */
    private static List<Map<String, Object>> readQuestions(String filePath) {
        List<Map<String, Object>> questionsList = new ArrayList<>();

        try (FileReader reader = new FileReader(new File(filePath))) {
            StringBuilder jsonBuilder = new StringBuilder();
            int a;
            while ((a = reader.read()) != -1) {
                jsonBuilder.append((char) a);
            }

            // Parses the JSON array
            JSONArray questionsArray = new JSONArray(jsonBuilder.toString());
            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject questionObject = questionsArray.getJSONObject(i);
                String question = questionObject.getString("question");
                JSONArray answersArray = questionObject.getJSONArray("answers");
                String correctAnswer = questionObject.getString("correctAnswer");

                List<String> answersList = new ArrayList<>();
                for (int j = 0; j < answersArray.length(); j++) {
                    answersList.add(answersArray.getString(j));
                }

                // Shuffle the answers
                Collections.shuffle(answersList);

                // Find the new position of the correct answer after shuffling
                int newCorrectIndex = answersList.indexOf(correctAnswer);

                // Store question, answers, and correct answer index in a map
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("question", question);
                questionData.put("answers", answersList);
                questionData.put("correctIndex", newCorrectIndex); // Store the index instead of the value

                questionsList.add(questionData); // Add question to the list
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + filePath);
            e.printStackTrace();
        }
        return questionsList;
    }
}
