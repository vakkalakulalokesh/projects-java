import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { WebsocketService } from '../../services/websocket.service';
import { LogEntry } from '../../models/execution.model';

@Component({
  selector: 'app-log-viewer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './log-viewer.component.html',
  styleUrl: './log-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogViewerComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly ws = inject(WebsocketService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('viewport') viewport?: ElementRef<HTMLDivElement>;

  readonly lines = signal<LogEntry[]>([]);
  readonly paused = signal(false);
  private buffer: LogEntry[] = [];

  ngOnInit(): void {
    this.ws
      .subscribeToLogs(this.id())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((entry) => {
        if (this.paused()) {
          this.buffer.push(entry);
        } else {
          this.lines.update((cur) => [...cur, entry]);
          this.cdr.markForCheck();
          queueMicrotask(() => this.scrollBottom());
        }
      });
  }

  togglePause(): void {
    const next = !this.paused();
    this.paused.set(next);
    if (!next && this.buffer.length) {
      const merged = [...this.lines(), ...this.buffer];
      this.buffer = [];
      this.lines.set(merged);
      this.cdr.markForCheck();
      queueMicrotask(() => this.scrollBottom());
    }
    this.cdr.markForCheck();
  }

  clear(): void {
    this.lines.set([]);
    this.buffer = [];
    this.cdr.markForCheck();
  }

  download(): void {
    const text = this.lines()
      .map((l) => `${l.timestamp}\t${l.level}\t${l.message}`)
      .join('\n');
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `execution-${this.id()}-logs.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  levelClass(level: string): string {
    const u = level?.toUpperCase() ?? 'INFO';
    if (u === 'ERROR') return 'lvl-error';
    if (u === 'WARN') return 'lvl-warn';
    if (u === 'DEBUG') return 'lvl-debug';
    return 'lvl-info';
  }

  private scrollBottom(): void {
    const el = this.viewport?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}
