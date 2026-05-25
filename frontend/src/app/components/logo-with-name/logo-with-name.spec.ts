import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogoWithName } from './logo-with-name';

describe('LogoWithName', () => {
  let component: LogoWithName;
  let fixture: ComponentFixture<LogoWithName>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogoWithName]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LogoWithName);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
