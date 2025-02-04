import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class QuizApp {
    private static final String FILE_PATH = "questions.json";  //Need to add json discrimination (difficukty and category)

    public static void main(String[] args) {
        //Read the JSON file and get the questions
        List<Map.Entry<String, List<String>>> questions=readQuestions(FILE_PATH);
        
        if (questions != null && !questions.isEmpty()) {
            //Select a random question from the list
            Random random = new Random();
            int randomIndex = random.nextInt(questions.size());
            
            //Get the question and answers
            Map.Entry<String, List<String>> selectedQuestion = questions.get(randomIndex);
            String question = selectedQuestion.getKey();
            List<String> answers = selectedQuestion.getValue();
            
            //Randomize answer order
            Collections.shuffle(answers);

            //Get the correct answer
            String correctAnswer = selectedQuestion.getValue().get(0); // Correct answer should be the first in list

            //Send question and correct answer to server, to then send to user
            
        }
    }

    //Read JSON function
    private static List<Map.Entry<String, List<String>>> readQuestions(String filePath) {
        List<Map.Entry<String, List<String>>> questionsList = new ArrayList<>();
        try {
            FileReader reader = new FileReader(new File(filePath));
            StringBuilder jsonBuilder = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                jsonBuilder.append((char) ch);
            }

            //Parse the JSON array
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
                //Ensure the correct answer is added first (it will be shuffled later)
                answersList.add(0, correctAnswer);

                //Store the question with its answers in the map
                questionsList.add(new AbstractMap.SimpleEntry<>(question, answersList));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return questionsList;
    }
}
