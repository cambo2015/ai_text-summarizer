import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserDashNav } from './user-dash-nav';

describe('UserDashNav', () => {
  let component: UserDashNav;
  let fixture: ComponentFixture<UserDashNav>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserDashNav]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserDashNav);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
