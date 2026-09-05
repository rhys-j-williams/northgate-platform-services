import axios, { AxiosInstance } from 'axios';
import { correlation } from './correlation';
import { config } from '../config';

/** Every outbound call carries the correlation id; Splunk joins the hops on it. */
export function upstream(baseURL: string): AxiosInstance {
  const client = axios.create({ baseURL, timeout: config.upstreamTimeoutMs });
  client.interceptors.request.use((req) => {
    req.headers = req.headers ?? {};
    req.headers['X-Correlation-Id'] = correlation.current();
    req.headers['X-Channel'] = 'BIZ';
    return req;
  });
  return client;
}
