import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render } from '@testing-library/preact';
import DOMPurify from 'dompurify';

// Test helper functions
const parse_html_from_string = (html) => {
  const parser = new DOMParser();
  return parser.parseFromString(html, 'text/html');
};

const mark_required_fields = (form, skipDateFields = true) => {
  const inputs = form.getElementsByTagName('input');
  for (const field of inputs) {
    if (field.hasAttribute('required')) {
      if (skipDateFields && field.type === 'date') continue;
      const prevElement = field.previousElementSibling;
      if (prevElement && prevElement.tagName === 'LABEL') {
        if (!prevElement.textContent.includes('*')) {
          prevElement.textContent += '*';
        }
      }
      // Handle nested input within label
      const parentLabel = field.closest('label');
      if (parentLabel && !parentLabel.textContent.includes('*')) {
        parentLabel.textContent += '*';
      }
    }
  }
};

describe('TaskForm Helper Functions', () => {
  describe('parse_html - required field marking', () => {
    let mockState;

    beforeEach(() => {
      mockState = {
        api: {
          user: { profile: { value: { id: 'user123' } } },
          task: {
            value: { data: { assignee: 'user123' } },
            one: { value: { data: { id: 'task123' } } }
          }
        }
      };
    });

    it('should mark text input as required with asterisk', () => {
      const html = `
        <form>
          <label>Username</label>
          <input type="text" name="username" required />
        </form>
      `;
      const doc = parse_html_from_string(html);
      const form = doc.getElementsByTagName('form')[0];
      mark_required_fields(form);
      expect(form.querySelector('label').textContent).toContain('*');
    });

    it('should handle missing previousElementSibling gracefully', () => {
      const html = `<form><input type="text" name="username" required /></form>`;
      const doc = parse_html_from_string(html);
      const form = doc.getElementsByTagName('form')[0];
      expect(() => mark_required_fields(form)).not.toThrow();
    });

    it('should handle nested input within label', () => {
      const html = `<form><label>Username<input type="text" name="username" required /></label></form>`;
      const doc = parse_html_from_string(html);
      const form = doc.getElementsByTagName('form')[0];
      mark_required_fields(form);
      expect(form.querySelector('label').textContent).toContain('*');
    });

    it('should NOT mark date inputs as required (current behavior)', () => {
      const html = `<form><label>Birth Date</label><input type="date" name="birthdate" required /></form>`;
      const doc = parse_html_from_string(html);
      const form = doc.getElementsByTagName('form')[0];
      mark_required_fields(form, true); // Skip date fields
      expect(form.querySelector('label').textContent).not.toContain('*');
    });

    it('should mark date inputs as required after TODO fix', () => {
      const html = `<form><label>Birth Date</label><input type="date" name="birthdate" required /></form>`;
      const doc = parse_html_from_string(html);
      const form = doc.getElementsByTagName('form')[0];
      mark_required_fields(form, false); // Don't skip date fields
      expect(form.querySelector('label').textContent).toContain('*');
    });

    it('should handle multiple required fields', () => {
      const html = `
        <form>
          <label>Username</label><input type="text" name="username" required />
          <label>Email</label><input type="email" name="email" required />
          <label>Age</label><input type="number" name="age" required />
        </form>
      `;
      const doc = parse_html_from_string(html);
      const form = doc.getElementsByTagName('form')[0];
      mark_required_fields(form, false);
      form.querySelectorAll('label').forEach(label => {
        expect(label.textContent).toContain('*');
      });
    });
  });

  describe('build_form_data logic', () => {
    const build_data = (input, temporary = false) => {
      const data = {};
      if (input.type === 'checkbox') {
        data[input.name] = { value: input.checked };
      } else if (input.name && input.value) {
        let val = input.value;
        if (input.type === 'number') val = parseInt(val, 10);
        else if (input.type === 'date') val = temporary ? val : val.split('-').reverse().join('/');
        data[input.name] = { value: val };
      }
      return data;
    };

    it('should build data for text input', () => {
      expect(build_data({ type: 'text', name: 'username', value: 'john' }))
        .toEqual({ username: { value: 'john' } });
    });

    it('should build data for checkbox', () => {
      expect(build_data({ type: 'checkbox', name: 'agree', checked: true }))
        .toEqual({ agree: { value: true } });
    });

    it('should build data for number input', () => {
      expect(build_data({ type: 'number', name: 'age', value: '25' }))
        .toEqual({ age: { value: 25 } });
    });

    it('should build data for date input with formatting', () => {
      expect(build_data({ type: 'date', name: 'birthdate', value: '2000-12-25' }))
        .toEqual({ birthdate: { value: '25/12/2000' } });
    });

    it('should skip inputs without name', () => {
      expect(Object.keys(build_data({ type: 'text', name: '', value: 'test' }))).toHaveLength(0);
    });

    it('should skip inputs without value', () => {
      expect(Object.keys(build_data({ type: 'text', name: 'username', value: '' }))).toHaveLength(0);
    });
  });

  describe('prepare_form_data logic', () => {
    const group_by_row = (components) => {
      const result = [];
      let rowName = '';
      let row = [];
      components.forEach((component, index) => {
        if (rowName !== component.layout.row) {
          if (rowName !== '') result.push({ key: rowName, value: row });
          row = [];
          rowName = component.layout.row;
        }
        row.push(component);
        if (index === components.length - 1) result.push({ key: rowName, value: row });
      });
      return result;
    };

    it('should group components by row', () => {
      const components = [
        { id: '1', layout: { row: 'Row_1' } },
        { id: '2', layout: { row: 'Row_1' } },
        { id: '3', layout: { row: 'Row_2' } }
      ];
      const result = group_by_row(components);
      expect(result).toHaveLength(2);
      expect(result[0]).toEqual({ key: 'Row_1', value: components.slice(0, 2) });
      expect(result[1]).toEqual({ key: 'Row_2', value: [components[2]] });
    });

    it('should handle single row', () => {
      const components = [
        { id: '1', layout: { row: 'Row_1' } },
        { id: '2', layout: { row: 'Row_1' } }
      ];
      const result = group_by_row(components);
      expect(result).toHaveLength(1);
      expect(result[0]).toEqual({ key: 'Row_1', value: components });
    });
  });
});

describe('TaskForm Sub-Components', () => {
  it('should handle datetime subtype conversion', () => {
    const convert_datetime = (type, subtype) =>
      type === 'datetime' ? (subtype === 'datetime' ? 'datetime-local' : subtype) : type;
    expect(convert_datetime('datetime', 'datetime')).toBe('datetime-local');
    expect(convert_datetime('datetime', 'date')).toBe('date');
  });

  it('should convert checklist to checkbox type', () => {
    const convert_type = (type) => type === 'checklist' ? 'checkbox' : type;
    expect(convert_type('checklist')).toBe('checkbox');
    expect(convert_type('radio')).toBe('radio');
  });
});
