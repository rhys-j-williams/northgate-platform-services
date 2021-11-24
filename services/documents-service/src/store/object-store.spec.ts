import { Readable } from 'stream';
import { LocalDiskObjectStore } from './object-store';

describe('LocalDiskObjectStore', () => {
  const store = new LocalDiskObjectStore();

  it('round trips an object and lists by prefix', async () => {
    const meta = await store.put('statements/ACC-1/2024-01.pdf', Readable.from([Buffer.from('%PDF-1.4 fake')]));
    expect(meta.size).toBe(13);
    expect(store.head('statements/ACC-1/2024-01.pdf')?.size).toBe(13);
    expect(store.head('statements/ACC-1/2024-02.pdf')).toBeUndefined();
    const chunks: Buffer[] = [];
    for await (const c of store.get('statements/ACC-1/2024-01.pdf')) {
      chunks.push(c as Buffer);
    }
    expect(Buffer.concat(chunks).toString()).toBe('%PDF-1.4 fake');
    expect(store.list('statements/ACC-1/').map((m) => m.key)).toEqual(['statements/ACC-1/2024-01.pdf']);
    expect(store.list('statements/ACC-nope/')).toEqual([]);
  });

  it('refuses keys that escape the root', () => {
    expect(() => store.head('../etc/passwd')).toThrow(/bad object key/);
    expect(() => store.head('/abs')).toThrow(/bad object key/);
  });
});
