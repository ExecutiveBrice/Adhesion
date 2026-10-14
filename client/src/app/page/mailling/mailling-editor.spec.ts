import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbConfig } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { MaillingComponent } from './mailling.component';
import { MailService } from '../../_services/mail.service';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { ApiRenderService } from '../../_services/api-render.service';

describe('Mailing editor font size', () => {
  let fixture: ComponentFixture<MaillingComponent>;
  let component: MaillingComponent;
  function refresh() { TestBed.inject(ApiRenderService).notify(); fixture.detectChanges(); }
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [MaillingComponent], providers: [provideRouter([]),
      { provide: MailService, useValue: { getHistorique: () => of([]) } },
      { provide: ActiviteService, useValue: { getAll: () => of([]), getAllNm1: () => of([]) } },
      { provide: ParamService, useValue: {} },
      { provide: ToastService, useValue: {} }
    ] });
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(MaillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => fixture.destroy());

  it('keeps typed text at the same size as the initial mail, including on a new line', async () => {
    await fixture.whenStable();
    component.content = 'Bonjour,<br><br>Amicalement';
    refresh();
    await fixture.whenStable();
    const editor: HTMLElement = fixture.nativeElement.querySelector('.angular-editor-textarea');
    const initialSize = getComputedStyle(editor).fontSize;
    const selection = window.getSelection()!;
    const range = document.createRange();
    range.setStart(editor.firstChild!, 'Bonjour,'.length);
    range.collapse(true);
    selection.removeAllRanges();
    selection.addRange(range);
    editor.focus();

    const type = (text: string) => {
      document.execCommand('insertText', false, text);
      editor.dispatchEvent(new Event('input', { bubbles: true }));
      editor.dispatchEvent(new KeyboardEvent('keyup', { key: text.slice(-1), bubbles: true }));
      const node = selection.focusNode!;
      const parent = node.nodeType === Node.TEXT_NODE ? node.parentElement! : node as HTMLElement;
      expect(getComputedStyle(parent).fontSize).toBe(initialSize);
    };

    type(' Voici le message');
    document.execCommand('insertParagraph');
    type('Une deuxième ligne');
    expect(component.content).toContain('Une deuxième ligne');
    expect(editor.textContent).toContain('Amicalement');
    selection.removeAllRanges();
  });

});
