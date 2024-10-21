import com.xh.question.Answer;
import com.xh.question.Question;

public class Application {
    public static void main(String[] args) throws Exception {
        Answer answer = new Answer();
        Question question = new Question();
        answer.boot(answer, question);
    }
}