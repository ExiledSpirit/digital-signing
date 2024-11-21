import { Component } from '@angular/core';
import { WebPkiService } from './web-pki.service';
import { SigningService } from './signing.service';
import { CertificateModel } from 'web-pki';
import { MatInputModule } from '@angular/material/input';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { lastValueFrom } from 'rxjs';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MatInputModule, ReactiveFormsModule, FormsModule, CommonModule, MatSelectModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  /**
   * Available user certificates.
   */
  certificates: CertificateModel[] = [];

  /**
   * Selected certificate.
   */
  selectedCertificate: CertificateModel | null = null;

  /**
   * Selected file.
   */
  selectedFile: File | null = null;
  
  /**
   * Base64 string of the selected file.
   */
  selectedFileBase64: string | null = null;

  /**
   * Loading flag.
   */
  loading = false;

  /**
   * Error message.
   */
  error: string | null = null;

  constructor(
    private webPkiService: WebPkiService,
    private signingService: SigningService
  ) {}

  async ngOnInit() {
    try {
      await this.initializeCertificates();
    } catch {
      this.setError('Failed to initialize WebPKI and load certificates!');
    }
  }

  /**
   * Load all user certificates.
   */
  async initializeCertificates() {
    await this.webPkiService.initialize();
    const certificates = await lastValueFrom(this.webPkiService.getCertificates());
    this.certificates = certificates.filter(cert => cert.pkiBrazil.cpf || cert.pkiBrazil.cnpj);
  }

  /**
   * On file selected handler.
   *
   * @param event
   */
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (this.isValidPdf(file)) {
      this.selectedFile = file;
      this.convertFileToBase64(file);
      this.clearError();
    } else {
      this.setError('Invalid PDF!');
    }
  }

  /**
   * Returns if given file is a valid PDF.
   *
   * @param file 
   * @returns 
   */
  isValidPdf(file: File): boolean {
    return file && file.type === 'application/pdf';
  }

  /**
   * Convert file to base64 string.
   *
   * @param file 
   * @param callback 
   */
  convertFileToBase64(file: Blob, callback?: (base64: string) => void) {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        const base64String = reader.result.split(',')[1];
        this.selectedFileBase64 = base64String;
        callback?.(base64String);
      }
    };
    reader.onerror = () => this.setError('Error reading file');
    reader.readAsDataURL(file);
  }

  /**
   * Sign PDF.
   * Prepares PDF to be signed in server-side and then applies signature.
   */
  async signPdf() {
    if (!this.isReadyToSign()) {
      this.setError('Invalid PDF or Certificate!');
      return;
    }

    this.startLoading();
    try {
      const certificateBase64 = await this.webPkiService.readCertificate(this.selectedCertificate!.thumbprint);
      const preparedPdfBytes = await this.preparePdf(certificateBase64);

      if (!preparedPdfBytes) {
        this.setError('Failed to prepare PDF!');
        return;
      }

      const fileBytes = this.convertBase64ToBlob(preparedPdfBytes.preparedPdfBytes);
      await this.signAndComplete(fileBytes, certificateBase64, preparedPdfBytes.toSignHash);
    } catch {
      this.setError('Failed to sign PDF');
    } finally {
      this.stopLoading();
    }
  }

  isReadyToSign(): boolean {
    return !!(this.selectedFile && this.selectedCertificate && this.selectedFileBase64);
  }

  /**
   * Prepares PDF server-side. Adds a signature field.
   *
   * @param certificateBase64 
   * @returns 
   */
  async preparePdf(certificateBase64: string) {
    return lastValueFrom(this.signingService.start(this.selectedFile!, certificateBase64, this.selectedCertificate!.thumbprint));
  }

  /**
   * Applies signature and download the singed PDF.
   *
   * @param fileBytes Original PDF file.
   * @param certificateBase64 Digital certificate as base64 string.
   * @param toSignHash Hash to be signed.
   */
  async signAndComplete(fileBytes: Blob, certificateBase64: string, toSignHash: string) {
    const signature = await this.webPkiService.signHash(toSignHash, this.selectedCertificate!.thumbprint, 'SHA-256');
    const signedPdfBlob = await lastValueFrom(this.signingService.complete(fileBytes, certificateBase64, signature, this.selectedCertificate!.thumbprint));
    this.downloadFile(signedPdfBlob, 'signed-document.pdf');
  }

  /**
   * Converts base64 string to Blob.
   *
   * @param base64
   * @returns 
   */
  convertBase64ToBlob(base64: string): Blob {
    const binaryData = atob(base64);
    const binaryArray = new Uint8Array(binaryData.length);
    for (let i = 0; i < binaryData.length; i++) {
      binaryArray[i] = binaryData.charCodeAt(i);
    }
    return new Blob([binaryArray], { type: 'application/pdf' });
  }

  /**
   * Download blob as file.
   *
   * @param blob 
   * @param filename 
   */
  downloadFile(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Clear errors and start loading.
   */
  startLoading() {
    this.loading = true;
    this.clearError();
  }

  /**
   * Stop loading.
   */
  stopLoading() {
    this.loading = false;
  }

  /**
   * Set error message.
   *
   * @param message
   */
  setError(message: string) {
    this.error = message;
  }

  /**
   * Clear errors.
   */
  clearError() {
    this.error = null;
  }
}
