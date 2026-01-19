import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface Plan {
  id: string;
  name: string;
  price: number;
  billingPeriod: string;
  maxUsers: number | null;
  maxRequestsPerMonth: number | null;
}

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-12 px-4">
      <div class="max-w-4xl mx-auto">
        <h1 class="text-3xl font-bold text-center mb-8">Escolha seu plano</h1>
        
        <div class="grid md:grid-cols-3 gap-6 mb-8">
          <div *ngFor="let plan of plans" 
               class="bg-white rounded-lg shadow p-6 cursor-pointer hover:shadow-lg transition"
               [class.ring-2]="selectedPlan?.id === plan.id"
               [class.ring-blue-500]="selectedPlan?.id === plan.id"
               (click)="selectPlan(plan)">
            <h3 class="text-xl font-bold mb-2">{{ plan.name }}</h3>
            <p class="text-3xl font-bold mb-4">R$ {{ plan.price }}<span class="text-sm text-gray-500">/mês</span></p>
            <ul class="space-y-2 text-sm text-gray-600">
              <li>✓ {{ plan.maxUsers || 'Ilimitado' }} usuários</li>
              <li>✓ {{ plan.maxRequestsPerMonth || 'Ilimitadas' }} demandas/mês</li>
            </ul>
          </div>
        </div>

        <div *ngIf="selectedPlan" class="bg-white rounded-lg shadow p-6 max-w-md mx-auto">
          <h2 class="text-xl font-bold mb-4">Dados da agência</h2>
          <form (ngSubmit)="signup()" #form="ngForm">
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">Nome da Agência</label>
              <input type="text" [(ngModel)]="agencyName" name="agencyName" required
                     class="w-full border rounded px-3 py-2">
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">Email do Admin</label>
              <input type="email" [(ngModel)]="adminEmail" name="adminEmail" required
                     class="w-full border rounded px-3 py-2">
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">Senha</label>
              <input type="password" [(ngModel)]="adminPassword" name="adminPassword" required
                     class="w-full border rounded px-3 py-2">
            </div>
            <button type="submit" [disabled]="!form.valid || loading"
                    class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50">
              {{ loading ? 'Processando...' : 'Continuar para pagamento' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class SignupComponent implements OnInit {
  plans: Plan[] = [];
  selectedPlan: Plan | null = null;
  agencyName = '';
  adminEmail = '';
  adminPassword = '';
  loading = false;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit() {
    this.http.get<Plan[]>('/api/onboarding/plans').subscribe(plans => {
      this.plans = plans;
    });
  }

  selectPlan(plan: Plan) {
    this.selectedPlan = plan;
  }

  signup() {
    if (!this.selectedPlan) return;

    this.loading = true;
    this.http.post<{ sessionUrl: string }>('/api/onboarding/signup', {
      planId: this.selectedPlan.id,
      agencyName: this.agencyName,
      adminEmail: this.adminEmail,
      adminPassword: this.adminPassword
    }).subscribe({
      next: (response) => {
        window.location.href = response.sessionUrl;
      },
      error: () => {
        this.loading = false;
        alert('Erro ao processar. Tente novamente.');
      }
    });
  }
}
