package extension;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B64HelperTest {

    private B64Helper b64Helper;

    @Before
    public void setup() {
        b64Helper = new B64Helper();
    }

    @Test
    public void encodeStringData() {
        String input = "xasxa";
        String expectedOutput = "eGFzeGE=";

        String actualOutput = b64Helper.encode(input.getBytes());

        assertEquals(actualOutput, expectedOutput);
    }
}