import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Recorder } from './recorder';

describe('Recorder', () => {
  let component: Recorder;
  let fixture: ComponentFixture<Recorder>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Recorder]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Recorder);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
