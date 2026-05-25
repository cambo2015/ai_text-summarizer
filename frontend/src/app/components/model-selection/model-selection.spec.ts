import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModelSelection } from './model-selection';

describe('ModelSelection', () => {
  let component: ModelSelection;
  let fixture: ComponentFixture<ModelSelection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelSelection]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ModelSelection);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
