import axios, { AxiosInstance } from 'axios';
import { correlation } from './correlation';
import { config } from '../config';

/** Every outbound call carries the correlation id; Splunk joins the hops on it. */
export function upstream(baseURL: string): AxiosInstance {
  const client = axios.create({ baseURL, timeout: config.upstreamTimeoutMs });
  client.interceptors.request.use((req) => {
    req.headers = req.headers ?? {};
    req.headers['X-Correlation-Id'] = correlation.current();
    // Same Keystone audience on both sides, so the customer's token is relayed rather than
    // minting a service token per hop. Keystone wanted per-service audiences in 2022 (KEY-3110)
    // and backed off; if that ever lands this needs an exchange step.
    const token = correlation.bearerToken();
    if (token && !req.headers['Authorization']) {
      req.headers['Authorization'] = `Bearer ${token}`;
    }
    req.headers['X-Channel'] = 'IRIS';
    return req;
  });
  return client;
}
