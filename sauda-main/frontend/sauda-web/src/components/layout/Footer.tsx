import { Link } from "react-router-dom";

export function Footer() {
  return (
    <footer className="mt-auto bg-navy-950 text-slate-300">
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-4">
          <div className="md:col-span-1">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-xs font-bold text-white">
                S
              </span>
              <span className="text-lg font-bold text-white">Sauda</span>
            </div>
            <p className="mt-4 text-sm leading-relaxed text-slate-400">
              Платформа корпоративных B2B закупок в Казахстане. Связываем покупателей и
              поставщиков.
            </p>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Покупателям
            </h3>
            <ul className="mt-4 space-y-2 text-sm">
              <li>
                <Link to="#" className="hover:text-white">
                  Каталог поставщиков
                </Link>
              </li>
              <li>
                <Link to="#" className="hover:text-white">
                  Мои заявки
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Поставщикам
            </h3>
            <ul className="mt-4 space-y-2 text-sm">
              <li>
                <Link to="/lots" className="hover:text-white">
                  Подходящие лоты
                </Link>
              </li>
              <li>
                <Link to="#" className="hover:text-white">
                  Загрузка прайса
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Компания
            </h3>
            <ul className="mt-4 space-y-2 text-sm">
              <li>
                <Link to="#" className="hover:text-white">
                  О платформе
                </Link>
              </li>
              <li>
                <Link to="#" className="hover:text-white">
                  Контакты
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-col gap-4 border-t border-slate-800 pt-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 Sauda. Все права защищены.</p>
          <div className="flex gap-4">
            <Link to="#" className="hover:text-slate-300">
              Политика конфиденциальности
            </Link>
            <Link to="#" className="hover:text-slate-300">
              Пользовательское соглашение
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
