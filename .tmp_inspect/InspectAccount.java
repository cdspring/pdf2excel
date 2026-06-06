
import com.pdf2excel.model.*;
import com.pdf2excel.service.PdfParseService;
import java.io.*;
public class InspectAccount {
  public static void main(String[] args) throws Exception {
    PdfParseService svc = new PdfParseService();
    BillData data;
    try (FileInputStream fis = new FileInputStream(args[0])) { data = svc.parse(fis); }
    for (PowerDetail d : data.getPowerDetails()) {
      if (args[1].equals(d.getAccountId())) {
        System.out.println(d.getSeq()+"|"+d.getAccountId()+"|"+d.getMeterId()+"|"+d.getPrevReading()+"|"+d.getCurrReading()+"|"+d.getMultiplier()+"|"+d.getLineLoss()+"|"+d.getTransformerLoss()+"|"+d.getRefund()+"|"+d.getPower());
      }
    }
  }
}
