import { CacheService } from './cache.service';

describe('CacheService (in process mode)', () => {
  it('stores and expires values', async () => {
    const cache = new CacheService();
    expect(cache.mode).toBe('memory');
    await cache.set('k', { a: 1 }, 60);
    expect(await cache.get('k')).toEqual({ a: 1 });
    await cache.del('k');
    expect(await cache.get('k')).toBeUndefined();
  });

  it('getOrLoad calls the loader once', async () => {
    const cache = new CacheService();
    const loader = jest.fn().mockResolvedValue('v');
    expect(await cache.getOrLoad('x', loader)).toBe('v');
    expect(await cache.getOrLoad('x', loader)).toBe('v');
    expect(loader).toHaveBeenCalledTimes(1);
  });

  it('drops entries past their ttl', async () => {
    const cache = new CacheService();
    const now = Date.now();
    const spy = jest.spyOn(Date, 'now').mockReturnValue(now);
    await cache.set('t', 1, 1);
    spy.mockReturnValue(now + 1500);
    expect(await cache.get('t')).toBeUndefined();
    spy.mockRestore();
  });
});
