import { createReadStream, createWriteStream, existsSync, mkdirSync, readdirSync, statSync } from 'fs';
import { dirname, join, relative, sep } from 'path';
import { pipeline } from 'stream/promises';
import { Readable } from 'stream';
import { config } from '../config';

export interface ObjectMeta {
  key: string;
  size: number;
  lastModified: string;
}

/**
 * Minimal S3-shaped interface. The real implementation (S3ObjectStore, PLAT-0740) was deleted from
 * this repo when the SDK moved to the shared appliance client; what is left is the local disk stub
 * that the tests and the demo use. Keys are slash separated, no leading slash, no `..`.
 */
export interface ObjectStore {
  put(key: string, body: Readable): Promise<ObjectMeta>;
  head(key: string): ObjectMeta | undefined;
  get(key: string): Readable;
  list(prefix: string): ObjectMeta[];
}

export class LocalDiskObjectStore implements ObjectStore {
  constructor(private readonly root = config.objectStoreRoot) {
    mkdirSync(root, { recursive: true });
  }

  async put(key: string, body: Readable): Promise<ObjectMeta> {
    const path = this.path(key);
    mkdirSync(dirname(path), { recursive: true });
    // write to a sidecar then rename would be correct; we do not, and a crash mid write leaves a
    // truncated object that is then served forever. INC0050271. TODO PLAT-1877.
    await pipeline(body, createWriteStream(path));
    return this.meta(key, path);
  }

  head(key: string): ObjectMeta | undefined {
    const path = this.path(key);
    return existsSync(path) ? this.meta(key, path) : undefined;
  }

  get(key: string): Readable {
    return createReadStream(this.path(key));
  }

  list(prefix: string): ObjectMeta[] {
    const dir = this.path(prefix);
    if (!existsSync(dir)) {
      return [];
    }
    const out: ObjectMeta[] = [];
    const walk = (d: string) => {
      for (const name of readdirSync(d)) {
        const p = join(d, name);
        if (statSync(p).isDirectory()) {
          walk(p);
        } else {
          out.push(this.meta(relative(this.root, p).split(sep).join('/'), p));
        }
      }
    };
    walk(dir);
    return out.sort((a, b) => a.key.localeCompare(b.key));
  }

  private path(key: string): string {
    if (key.startsWith('/') || key.split('/').includes('..') || key.length === 0) {
      throw new Error(`bad object key: ${key}`);
    }
    return join(this.root, ...key.split('/'));
  }

  private meta(key: string, path: string): ObjectMeta {
    const st = statSync(path);
    return { key, size: st.size, lastModified: st.mtime.toISOString() };
  }
}
