import { TestBed } from '@angular/core/testing';

import { TutorialServiceTs } from './tutorial-service.ts';

describe('TutorialServiceTs', () => {
  let service: TutorialServiceTs;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TutorialServiceTs);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
