import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MobileDashMenu } from './mobile-dash-menu';

describe('MobileDashMenu', () => {
  let component: MobileDashMenu;
  let fixture: ComponentFixture<MobileDashMenu>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MobileDashMenu]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MobileDashMenu);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
