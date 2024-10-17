import com.xh.question.Question;

import java.io.IOException;

public class Application {
    public static void main(String[] args) throws IOException {
        Question question = new Question();
        //获取所有的程序代码编号
        question.getCProgramID();
        //question.CProgramIDList.add("ch001_001");
        question.getQuestion();
    }
}