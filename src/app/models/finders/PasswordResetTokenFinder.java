package models.finders;

import io.ebean.Finder;
import models.NetService;
import models.PasswordResetToken;

import java.util.UUID;

public class PasswordResetTokenFinder extends Finder<UUID, PasswordResetToken> {
    public PasswordResetTokenFinder() {
        super(PasswordResetToken.class);
    }
}
