import { PwaService } from './pwa.service';

describe('Détection de la PWA mobile', () => {
  const keys = ['userAgent', 'maxTouchPoints', 'standalone'] as const;
  let original: Map<string, PropertyDescriptor | undefined>;
  let standalone = false;

  beforeEach(() => {
    original = new Map(keys.map(key => [key, Object.getOwnPropertyDescriptor(navigator, key)]));
    Object.defineProperty(navigator, 'maxTouchPoints', { configurable: true, value: 0 });
    Object.defineProperty(navigator, 'standalone', { configurable: true, value: false });
    standalone = false;
    spyOn(window, 'matchMedia').and.callFake(query => ({
      matches: query === '(display-mode: standalone)' && standalone
    } as MediaQueryList));
  });

  afterEach(() => {
    for (const key of keys) {
      const descriptor = original.get(key);
      if (descriptor) Object.defineProperty(navigator, key, descriptor);
      else Reflect.deleteProperty(navigator, key);
    }
  });

  it('reconnaît une application Android installée', () => {
    Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'Android' });
    standalone = true;
    expect(new PwaService().isMobileStandalone()).toBeTrue();
  });

  it('reconnaît une application iOS via navigator.standalone', () => {
    Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'iPhone' });
    Object.defineProperty(navigator, 'standalone', { configurable: true, value: true });
    expect(new PwaService().isMobileStandalone()).toBeTrue();
  });

  it('exclut le navigateur mobile et une PWA sur ordinateur', () => {
    Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'Android' });
    expect(new PwaService().isMobileStandalone()).toBeFalse();
    Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'Desktop' });
    standalone = true;
    expect(new PwaService().isMobileStandalone()).toBeFalse();
  });
});
