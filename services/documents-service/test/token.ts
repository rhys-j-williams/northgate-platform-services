export function testToken(claims: Record<string, unknown>): string {
  const b64 = (o: unknown) => Buffer.from(JSON.stringify(o)).toString('base64url');
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ iss: 'http://localhost:4400', aud: 'meridian-retail', exp: Math.floor(Date.now() / 1000) + 300, ...claims })}.`;
}
