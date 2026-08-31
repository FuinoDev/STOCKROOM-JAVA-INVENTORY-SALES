package ph.stockroom.util;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class CsvExporter {
    private CsvExporter() { }
    public static String cell(Object value) {
        String text=value==null?"":value.toString();
        // Prevent spreadsheet applications from treating untrusted product names as formulas.
        String leading=text.stripLeading();
        if(value instanceof CharSequence && !leading.isEmpty() && "=+-@".indexOf(leading.charAt(0))>=0) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
    public static void write(Path path,List<String> headings,List<? extends List<?>> rows) throws IOException {
        try(var writer=Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');writeRow(writer,headings);
            for(var row:rows) writeRow(writer,row);
        }
    }
    private static void writeRow(Writer writer,List<?> row) throws IOException {
        for(int i=0;i<row.size();i++) { if(i>0) writer.write(',');writer.write(cell(row.get(i))); }writer.write("\r\n");
    }
}
