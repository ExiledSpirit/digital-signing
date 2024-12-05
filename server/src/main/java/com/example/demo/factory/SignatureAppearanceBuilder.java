// SignatureAppearanceBuilder.java (New Builder Class)
package com.example.demo.factory;

import java.io.IOException;
import java.util.Map;

import com.example.demo.signature.SignatureConstraints;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.signatures.PdfSignatureAppearance;

public class SignatureAppearanceBuilder {
    private final float baseLowerLeftX = 0f;
    private final float baseLowerLeftY = 0f;
    private final PdfSignatureAppearance appearance;
    private final PdfDocument document;
    private int page = 1;
    private float lowerLeftX = this.baseLowerLeftX;
    private float lowerLeftY = this.baseLowerLeftY;
    private float width = 186f;
    private float height = 47f;
    private int fontSize = 8;
    private String layer2Text = "Assinado Digitalmente por\n NOME DO USUÁRIO\n (Emitido pelo CPF 690.XXX.XXX-20)\n Data: 24/09/2024 11:56:28-03:00";

    public SignatureAppearanceBuilder(PdfDocument document, PdfSignatureAppearance appearance, boolean lastPage) {
        if (lastPage) this.page = document.getNumberOfPages();
        this.document = document;
        this.appearance = appearance;
        Rectangle calculatedRectangle = this.adjustPositionForPage();
        this.lowerLeftX = calculatedRectangle.getX();
        this.lowerLeftY = calculatedRectangle.getY();
    }

    public SignatureAppearanceBuilder onPageNumber(int page) {
        this.page = page;
        return this;
    }

    public SignatureAppearanceBuilder withPosition(float lowerLeftX, float lowerLeftY) {
        this.lowerLeftX = lowerLeftX;
        this.lowerLeftY = lowerLeftY;
        return this;
    }

    public SignatureAppearanceBuilder withSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public SignatureAppearanceBuilder withLayer2Text(String layer2Text) {
        this.layer2Text = layer2Text;
        return this;
    }

    public PdfSignatureAppearance build() {
        Rectangle rect = new Rectangle(lowerLeftX, lowerLeftY, width, height);
        PdfFont font;

        try {
            font = PdfFontFactory.createFont("src/main/resources/fonts/Arial.ttf");
        } catch (IOException e) {
            font = null;
        }

        appearance
            .setPageRect(rect)
            .setPageNumber(page)
            .setLayer2Text(layer2Text)
            .setLayer2FontSize(fontSize)
            .setLayer2Font(font);
        return appearance;
    }

    private Rectangle adjustPositionForPage() {
        int fieldCount = this.getFieldCount();

        float pageWidth = document.getPage(page).getCropBox().getWidth();

        int razao = (int) Math.floor(pageWidth/this.width) ; // ex: W = w * x => 760 = 190 * x => x = 760/190 => x = 4

        int column = fieldCount % razao; // ex: razao = 4 & fieldCount = 9 => column = 1 
        int row = (int) Math.ceil(fieldCount/razao); // ex: razao = 4 & fieldCount = 9 => row = 3
        float lowerLeftX = Math.max(this.width * column, this.baseLowerLeftX);
        float lowerLeftY = Math.max(this.height * row, this.baseLowerLeftY);
        return new Rectangle(lowerLeftX, lowerLeftY, this.width, this.height);
    }

    private int getFieldCount() {
        Map<String, PdfFormField> fields = PdfAcroForm.getAcroForm(this.document, false).getFormFields();
        int fieldCount = 0;

        fieldCount = (int) fields.keySet().stream().filter(key -> key.startsWith(SignatureConstraints.FIELD_NAME)).count();

        return fieldCount;
    }

    public String calculateFieldName() {
        return SignatureConstraints.FIELD_NAME + this.getFieldCount();
    }
}
