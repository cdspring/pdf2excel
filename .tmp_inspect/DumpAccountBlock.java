
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.*; import java.util.*;
public class DumpAccountBlock {
  public static void main(String[] args) throws Exception {
    PDDocument doc = PDDocument.load(new File(args[0]));
    PDFTextStripper s = new PDFTextStripper();
    s.setStartPage(1); s.setEndPage(doc.getNumberOfPages());
    String[] arr = s.getText(doc).split("\n");
    List<String> lines = new ArrayList<>();
    for (String line: arr) lines.add(line.trim());
    String a1 = args[1], a2 = args[2];
    for (int i=0;i<lines.size()-1;i++) {
      if (lines.get(i).equals(a1) && lines.get(i+1).equals(a2)) {
        System.out.println("FOUND at index="+i);
        for (int j=Math.max(0,i-15); j<Math.min(lines.size(), i+40); j++) {
          System.out.println(j+": "+lines.get(j));
        }
      }
    }
    doc.close();
  }
}
