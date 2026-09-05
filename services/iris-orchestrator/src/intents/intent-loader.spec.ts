import { IntentLoader } from './intent-loader';

describe('IntentLoader', () => {
  it('loads the shipped yaml with a fallback and unique ids', () => {
    const loader = new IntentLoader();
    const all = loader.all();
    expect(all.length).toBeGreaterThan(8);
    expect(new Set(all.map((i) => i.id)).size).toBe(all.length);
    expect(loader.fallback().id).toBe('fallback');
    expect(loader.byId('dispute')?.handoff).toBe(true);
    expect(loader.byId('dispute')?.disclosure).toBe('reg_e_dispute');
  });

  it('rejects a file without a fallback', () => {
    process.env.IRIS_INTENTS_FILE = __dirname + '/../../test/bad-intents.yaml';
    jest.resetModules();
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const { IntentLoader: Fresh } = require('./intent-loader');
    expect(() => new Fresh().load()).toThrow(/fallback/);
    delete process.env.IRIS_INTENTS_FILE;
  });
});
