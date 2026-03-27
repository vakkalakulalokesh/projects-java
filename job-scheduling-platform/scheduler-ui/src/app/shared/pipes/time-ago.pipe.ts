import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeAgo',
  standalone: true,
  pure: true,
})
export class TimeAgoPipe implements PipeTransform {
  transform(iso: string | undefined | null, _refreshToken?: number): string {
    if (!iso) {
      return '—';
    }
    const then = new Date(iso).getTime();
    if (Number.isNaN(then)) {
      return '—';
    }
    const seconds = Math.floor((Date.now() - then) / 1000);
    if (seconds < 0) {
      return 'just now';
    }
    if (seconds < 60) {
      return `${seconds}s ago`;
    }
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) {
      return `${minutes}m ago`;
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return `${hours}h ago`;
    }
    const days = Math.floor(hours / 24);
    if (days < 30) {
      return `${days}d ago`;
    }
    const months = Math.floor(days / 30);
    if (months < 12) {
      return `${months}mo ago`;
    }
    return `${Math.floor(months / 12)}y ago`;
  }
}
