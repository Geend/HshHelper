package extension;

import ch.compile.recaptcha.ReCaptchaVerify;
import ch.compile.recaptcha.model.SiteVerifyResponse;
import org.apache.commons.lang3.StringUtils;
import play.Play;
import play.twirl.api.Html;

import java.io.IOException;

public class RecaptchaHelper {
    /*
        Benötigt Hash in application.config:contentSecurityPolicy
     */
    public static Html CaptchaField() {
        String rcPublicKey = Play.application().configuration().getString("recaptcha.publicKey");
        return  new Html("<script src='https://www.google.com/recaptcha/api.js'></script>"+
                "<div class=\"g-recaptcha\" data-sitekey=\""+rcPublicKey+"\" data-callback=\"captchaCallback\"></div>"+
                "<input type=\"hidden\" name=\"recaptcha\" id=\"recaptcha\" />"+
                "<script>\n" +
                "  function captchaCallback() {\n" +
                "    document.getElementById(\"recaptcha\").value = document.getElementById(\"g-recaptcha-response\").value;\n" +
                "  }\n" +
                "</script>");
    }

    public static boolean IsValidResponse(String response, String remoteIp) {
        if(StringUtils.isEmpty(response))
            return false;

        String rcPrivateKey = Play.application().configuration().getString("recaptcha.privateKey");

        ReCaptchaVerify reCaptchaVerify = new ReCaptchaVerify(rcPrivateKey);
        SiteVerifyResponse siteVerifyResponse = null;

        // ugly but easing things up.. :/
        try {
            siteVerifyResponse = reCaptchaVerify.verify(response, remoteIp);
        } catch (IOException e) {
            return false;
        }

        return siteVerifyResponse.isSuccess();
    }
}
