package extension;

import models.PermissionLevel;

public class PermissionLevelConverter {
    public static PermissionLevel FromReadWrite(boolean canRead, boolean canWrite) {
        if(canRead && canWrite) {
            return PermissionLevel.READWRITE;
        }
        if(canWrite) {
            return PermissionLevel.WRITE;
        }
        else if(canRead) {
            return PermissionLevel.READ;
        }
        else {
            return PermissionLevel.NONE;
        }
    }

    public static CanReadWrite ToReadWrite(PermissionLevel level) {
        CanReadWrite result = new CanReadWrite();
        switch (level) {
            case READ:
                result.setCanRead(true);
                result.setCanWrite(false);
                break;
            case WRITE:
                result.setCanRead(false);
                result.setCanWrite(true);
                break;
            case READWRITE:
                result.setCanRead(true);
                result.setCanWrite(true);
                break;
        }
        return result;
    }
}

