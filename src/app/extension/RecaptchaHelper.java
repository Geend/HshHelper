package extension;

import ch.compile.recaptcha.ReCaptchaVerify;
import ch.compile.recaptcha.model.SiteVerifyResponse;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.io.IOException;

public class RecaptchaHelper {

    private final Config config;

    @Inject
    public RecaptchaHelper(Config config) {
        this.config = config;
    }

    public Html CaptchaField() {
        String rcPublicKey = config.getString("recaptcha.publicKey");
        return  new Html("<script src='https://www.google.com/recaptcha/api.js'></script>"+
                "<div class=\"g-recaptcha\" data-sitekey=\""+rcPublicKey+"\"></div>");
    }

    public boolean IsValidResponse(String response, String remoteIp) {
        if(StringUtils.isEmpty(response))
            return false;

        String rcPrivateKey = config.getString("recaptcha.privateKey");

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
