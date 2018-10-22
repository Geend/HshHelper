package extension;

import ch.compile.recaptcha.ReCaptchaVerify;
import ch.compile.recaptcha.model.SiteVerifyResponse;
import play.Play;
import play.twirl.api.Html;

import java.io.IOException;

public class RecaptchaHelper {
    public static Html CaptchaField() {
        String rcPublicKey = Play.application().configuration().getString("recaptcha.publicKey");
        return  new Html("<script src='https://www.google.com/recaptcha/api.js'></script>"+
                "<div class=\"g-recaptcha\" data-sitekey=\""+rcPublicKey+"\"></div>");
    }

    public static boolean IsValidResponse(String response) throws IOException {
        String rcPrivateKey = Play.application().configuration().getString("recaptcha.privateKey");

        ReCaptchaVerify reCaptchaVerify = new ReCaptchaVerify(rcPrivateKey);
        SiteVerifyResponse siteVerifyResponse = reCaptchaVerify.verify(response, "127.0.0.1");

        return siteVerifyResponse.isSuccess();
    }
}
