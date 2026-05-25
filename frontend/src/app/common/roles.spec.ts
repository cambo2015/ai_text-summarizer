import { Role } from './roles';

describe('Roles', () => {
  it('should create an instance', () => {
    expect(new Role("ROLE_ADMIN")).toBeTruthy();
  });
});
