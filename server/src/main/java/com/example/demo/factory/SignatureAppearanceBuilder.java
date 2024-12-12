// SignatureAppearanceBuilder.java (New Builder Class)
package com.example.demo.factory;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import com.example.demo.signature.SignatureConstraints;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
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
    private String text = "Assinado Digitalmente por\n NOME DO USUÁRIO\n (Emitido pelo CPF 690.XXX.XXX-20)\n Data: 24/09/2024 11:56:28-03:00";
    private StampType stampType = StampType.ICP;

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

    public SignatureAppearanceBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public SignatureAppearanceBuilder withStamp(StampType stampType) {
        this.stampType = stampType;
        return this;
    }

    public PdfSignatureAppearance build() {
        Rectangle rect = new Rectangle(lowerLeftX, lowerLeftY, width, height);
        int fontSize = this.fontSize;
        PdfFont font;

        try {
            font = PdfFontFactory.createFont("fonts/Arial.ttf");
        } catch (IOException e) {
            font = null;
        }

        appearance
            .setPageRect(rect)
            .setPageNumber(page);

        PdfFormXObject layer2XObject = this.appearance.getLayer2();

        try {
            Image stamp = this.fetchStamp();
            PdfFormXObject container = new PdfFormXObject(new Rectangle(rect.getWidth(), rect.getHeight()));

            // Create paragraph.
            Paragraph paragraph = new Paragraph(this.text)
                    .setMargin(0)
                    .setPadding(0)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setTextAlignment(TextAlignment.LEFT);


            // Calculate dimensions and positions of stamp
            float stampWidth = 40f; // Adjust as needed
            float availableTextWidth = rect.getWidth() - stampWidth - 5f;

            float stampHeight = stamp.getImageScaledHeight() * stampWidth / stamp.getImageScaledWidth(); // Maintain aspect ratio
            float stampX = container.getWidth() - stampWidth - 5f;
            float stampY = (container.getHeight() - stampHeight) / 2;

            // Set fixed dimensions for the stamp
            stamp.scaleToFit(stampWidth, stampHeight);
            stamp.setFixedPosition(stampX, stampY);

            // Create div and assign text and stamp to it.
            Div div = new Div();
            div.add(paragraph.setWidth(availableTextWidth)).add(stamp);

            // Add div containing text and stamp to the canvas
            Canvas canvas = new Canvas(layer2XObject, document);
            canvas.add(div);
            canvas.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private Image fetchStamp() throws IOException {
        ClassPathResource logoResource = new ClassPathResource(this.stampType.equals(StampType.Logo) ? "images/logo.png" : "images/icpbrasil.jpg");
        byte[] logoBytes = logoResource.getInputStream().readAllBytes();
        PdfImageXObject logoImageXObject = new PdfImageXObject(ImageDataFactory.create(logoBytes));
        return new Image(logoImageXObject);
    }

    public static enum StampType {
        ICP,
        Logo;
    }
}
