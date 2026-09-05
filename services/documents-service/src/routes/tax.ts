import { Router } from 'express';
import { Readable } from 'stream';
import { ObjectStore } from '../store/object-store';
import { ApiError } from '../common/api-error';
import { fixtures } from '../fixtures';

/**
 * Tax documents come from the tax vendor's SFTP drop in January and are loaded into the object
 * store by the batch job (documents-tax-loader, separate repo). This service only lists and serves.
 * Locally nothing loads them so we synthesise a placeholder PDF per interest bearing account for
 * the previous tax year the first time anyone asks. The placeholder is a real (tiny) PDF so the
 * browser viewer opens it; it says PLACEHOLDER in 40pt.
 */
export function taxRouter(store: ObjectStore): Router {
  const r = Router();

  r.get('/tax', (req, res) => {
    const customerId = req.principal!.customerId;
    const year = new Date().getFullYear() - 1;
    const docs = fixtures()
      .accounts.filter((a) => a.customerId === customerId && (a.type === 'savings' || a.type === 'certificate' || a.type === 'business-savings'))
      .map((a) => {
        const k = key(customerId, year, a.accountId);
        if (!store.head(k)) {
          void store.put(k, Readable.from([placeholderPdf(`Form 1099-INT ${year} PLACEHOLDER`)]));
        }
        return { form: '1099-INT', taxYear: year, accountId: a.accountId, href: `/documents/v1/tax/${year}/${a.accountId}.pdf` };
      });
    res.json(docs);
  });

  r.get('/tax/:year/:accountId.pdf', (req, res, next) => {
    const k = key(req.principal!.customerId, Number(req.params.year), req.params.accountId);
    const meta = store.head(k);
    if (!meta) {
      next(new ApiError(404, 'DOCUMENT_NOT_FOUND', 'no such tax document'));
      return;
    }
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Length', String(meta.size));
    store.get(k).pipe(res);
  });

  return r;
}

function key(customerId: string, year: number, accountId: string): string {
  return `tax/${customerId}/${year}/1099-INT-${accountId}.pdf`;
}

/** Hand assembled single page PDF. Do not extend; if you need real rendering call statements-api. */
export function placeholderPdf(text: string): Buffer {
  const content = `BT /F1 40 Tf 60 400 Td (${text.replace(/[()\\]/g, '')}) Tj ET`;
  const objs = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
    `<< /Length ${content.length} >>\nstream\n${content}\nendstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  ];
  let body = '%PDF-1.4\n';
  const offsets: number[] = [];
  objs.forEach((o, i) => {
    offsets.push(body.length);
    body += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = body.length;
  body += `xref\n0 ${objs.length + 1}\n0000000000 65535 f \n`;
  for (const off of offsets) {
    body += `${String(off).padStart(10, '0')} 00000 n \n`;
  }
  body += `trailer\n<< /Size ${objs.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(body, 'latin1');
}
