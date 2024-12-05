import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import type { PreparePdfResponse } from './prepare-pdf-response';

@Injectable({
  providedIn: 'root'
})
export class SigningService {
  private apiUrl = 'http://localhost:4200/api/remote-signing';

  constructor(private http: HttpClient) { }

  /**
   * Initiate the signing process by sending the PDF so be signed and certificate to the server.
   * The server will then return us the new PDF byte so we can sign it here in the client.
   * 
   * @param file The PDF file to sign.
   * @param certificateBase64 The user's certificate in Base64 format.
   * @param certificateThumbprint The certificate thumbprint.
   */
  start(file: File, certificateBase64: string, certificateThumbprint: string): Observable<PreparePdfResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('certContent', certificateBase64);
    formData.append('certThumb', certificateThumbprint);

    return this.http.post<PreparePdfResponse>(`${this.apiUrl}/start`, formData, {withCredentials: true})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Completes the signing process, sending the previously prepared PDF bytes, the
   * signature hash and the certificate in base64 format. We are also sending the
   * certificate thumbprint but its not really being used in our API.
   *
   * @param preparedPdfBytes The pdf after received in the start process. Prefereably the backend should
   * keep the pdf stored waiting for the complete step so we dont need to pass it again.
   * @param certificateBase64 Certificate in base64 format.
   * @param signedHash The signature hash.
   * @param certificateThumbprint The certificate thumbprint.
   * @returns 
   */
  complete(signedHash: string) {
    const formData = new FormData();
    formData.append('signedHash', signedHash);
    return this.http.post(`${this.apiUrl}/complete`, formData, {responseType: 'blob', withCredentials: true})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Error handling for HTTP requests.
   * 
   * @param error Error response from the server.
   */
  private handleError(error: HttpErrorResponse) {
    console.error('HTTP Error:', error);
    return throwError(() => new Error('An error occurred during the PDF signing process.'));
  }
}
