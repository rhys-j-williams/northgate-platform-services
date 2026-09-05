import { Router } from 'express';
import { existsSync, readdirSync, readFileSync } from 'fs';
import { join } from 'path';
import { ApiError } from '../common/api-error';

/**
 * Disclosure HTML fragments rendered inside cn-disclosure in the front ends. Public: no token,
 * because the fee schedule and privacy notice are shown pre-login. Compliance owns the wording
 * (CMP-0412); engineering owns the markup. Versioned by the `data-version` attribute on the root
 * element, which the audit trail records when a customer accepts one.
 */
export function disclosuresRouter(dir = join(__dirname, '..', '..', 'disclosures')): Router {
  const r = Router();

  r.get('/disclosures', (_req, res) => {
    res.json(
      readdirSync(dir)
        .filter((f) => f.endsWith('.html'))
        .map((f) => f.replace(/\.html$/, ''))
        .sort()
        .map((key) => ({ key, href: `/documents/v1/disclosures/${key}` })),
    );
  });

  r.get('/disclosures/:key', (req, res, next) => {
    const key = req.params.key;
    if (!/^[a-z0-9_-]+$/.test(key)) {
      next(new ApiError(400, 'DISCLOSURE_KEY', 'bad disclosure key'));
      return;
    }
    const file = join(dir, `${key}.html`);
    if (!existsSync(file)) {
      next(new ApiError(404, 'DISCLOSURE_NOT_FOUND', `no disclosure ${key}`));
      return;
    }
    const html = readFileSync(file, 'utf8');
    const version = /data-version="([^"]+)"/.exec(html)?.[1] ?? 'unversioned';
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.setHeader('X-Disclosure-Version', version);
    res.setHeader('Cache-Control', 'public, max-age=300');
    res.send(html);
  });

  return r;
}
