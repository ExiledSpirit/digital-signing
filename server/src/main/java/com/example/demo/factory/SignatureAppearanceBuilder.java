// SignatureAppearanceBuilder.java (New Builder Class)
package com.example.demo.factory;

import java.util.Map;

import com.example.demo.signature.SignatureConstraints;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.signatures.PdfSignatureAppearance;

public class SignatureAppearanceBuilder {
    private final float baseLowerLeftX = 100f;
    private final float baseLowerLeftY = 100f;
    private final PdfSignatureAppearance appearance;
    private final PdfDocument document;
    private int page = 1;
    private float lowerLeftX = this.baseLowerLeftX;
    private float lowerLeftY = this.baseLowerLeftY;
    private float width = 150f;
    private float height = 100f;
    private String layer2Text = "Signed";


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

        appearance.setPageRect(rect).setPageNumber(page).setLayer2Text(layer2Text);
        return appearance;
    }

    private Rectangle adjustPositionForPage() {
        int fieldCount = this.getFieldCount();

        float pageWidth = document.getPage(page).getCropBox().getWidth();
        float widthSum = (this.lowerLeftX + (fieldCount + 1) * this.width);
        float lowerLeftX = Math.max((widthSum % pageWidth) - this.width, this.lowerLeftX);
        float lowerLeftY = ((float) Math.ceil(pageWidth / widthSum)) * this.lowerLeftY;
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
