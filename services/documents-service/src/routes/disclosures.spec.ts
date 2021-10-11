import express from 'express';
import request from 'supertest';
import { disclosuresRouter } from './disclosures';
import { errorHandler } from '../common/api-error';

// Router level only. createApp() pulls in the JWT layer and the statements client, and stubbing
// statements-api properly is PLAT-1877. Until then the statements and tax routes are exercised by
// smoke.sh, not by Jest, which is why the Sonar number for this service is what it is.
describe('disclosures', () => {
  const app = express().use('/documents/v1', disclosuresRouter()).use(errorHandler);

  it('lists the shipped disclosures', async () => {
    const res = await request(app).get('/documents/v1/disclosures').expect(200);
    expect(res.body.map((d: { key: string }) => d.key)).toEqual(['esign', 'fee_schedule', 'privacy_notice', 'reg_e_dispute']);
  });

  it('serves html with the version header', async () => {
    const res = await request(app).get('/documents/v1/disclosures/reg_e_dispute').expect(200);
    expect(res.headers['content-type']).toMatch(/text\/html/);
    expect(res.headers['x-disclosure-version']).toBe('2023-11-15');
    expect(res.text).toContain('60 days');
  });

  it('404s with the standard envelope', async () => {
    const res = await request(app).get('/documents/v1/disclosures/nope').expect(404);
    expect(res.body).toMatchObject({ code: 'DISCLOSURE_NOT_FOUND', status: 404 });
  });
});
