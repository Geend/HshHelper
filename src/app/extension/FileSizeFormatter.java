package extension;

public class FileSizeFormatter {
    public static String FormatSize(long sizeInByte) {
        return FormatSize((double)sizeInByte);
    }

    public static String FormatSize(double sizeInByte) {
        String[] names = new String[8];
        names[0] = "Byte";
        names[1] = "Kilobyte";
        names[2] = "Megabyte";
        names[3] = "Gigabyte";
        names[4] = "Terabyte";
        names[5] = "Petabyte";
        names[6] = "Exabyte";
        names[7] = "Zettabyte";

        int finalIndex = 0;
        for(int i = 0; i < 6; i++) {
            if(sizeInByte > 1000) {
                sizeInByte = sizeInByte / 1000.0;
                finalIndex++;
            }
            else {
                break;
            }
        }
        return String.format("%s %s", sizeInByte, names[finalIndex]);
    }
}
