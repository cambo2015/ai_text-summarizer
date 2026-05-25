import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommonWordChips } from './common-word-chips';

describe('CommonWordChips', () => {
  let component: CommonWordChips;
  let fixture: ComponentFixture<CommonWordChips>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonWordChips]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CommonWordChips);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
