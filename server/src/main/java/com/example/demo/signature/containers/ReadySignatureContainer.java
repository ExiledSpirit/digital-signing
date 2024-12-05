package com.example.demo.signature.containers;

import java.io.InputStream;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.signatures.IExternalSignatureContainer;

public class ReadySignatureContainer implements IExternalSignatureContainer {
  private byte[] cmsSignatureContents;

  public ReadySignatureContainer(byte[] cmsSignatureContents) {
      this.cmsSignatureContents = cmsSignatureContents;
  }

  public byte[] sign(InputStream docBytes) {
      return cmsSignatureContents;
  }

  public void modifySigningDictionary(PdfDictionary signDic) {
  }
}
