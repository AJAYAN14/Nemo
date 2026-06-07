import androidx.graphics.shapes.Morph;
import java.lang.reflect.Method;

public class TestMorph {
    public static void main(String[] args) {
        for (Method m : Morph.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }
}
